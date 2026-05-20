# server_with_vectordb.py
from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import ollama
import chromadb
from chromadb.utils import embedding_functions
import os
import uuid
import PyPDF2
from docx import Document

# ========== FASTAPI APP ==========
app = FastAPI(title="Local LLM with Vector DB")

# ========== VECTOR DATABASE SETUP ==========
print("🔄 Initializing Vector Database...")
chroma_client = chromadb.PersistentClient(path="./vector_db")

# Embedding function (converts text to vectors)
embedding_function = embedding_functions.SentenceTransformerEmbeddingFunction(
    model_name="all-MiniLM-L6-v2"
)

# Get or create collection
try:
    collection = chroma_client.get_collection(
        name="my_documents",
        embedding_function=embedding_function
    )
    print(f"✅ Loaded existing collection with {collection.count()} documents")
except:
    collection = chroma_client.create_collection(
        name="my_documents",
        embedding_function=embedding_function
    )
    print("✅ Created new collection")

# ========== DATA MODELS ==========
class ChatRequest(BaseModel):
    message: str
    use_rag: Optional[bool] = False
    latitude: Optional[float] = None
    longitude: Optional[float] = None

class ChatResponse(BaseModel):
    reply: str
    sources: Optional[List[str]] = None

# ========== DOCUMENT PROCESSING ==========
def extract_text_from_pdf(file_path: str) -> str:
    """Improved PDF text extraction with error handling"""
    try:
        text = ""
        with open(file_path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            
            # Check if PDF is encrypted
            if pdf_reader.is_encrypted:
                try:
                    pdf_reader.decrypt('')
                except:
                    raise Exception("PDF is password protected and cannot be decrypted")
            
            print(f"📑 PDF has {len(pdf_reader.pages)} pages")
            
            # Extract text from each page
            for page_num, page in enumerate(pdf_reader.pages):
                try:
                    page_text = page.extract_text()
                    if page_text:
                        text += page_text + "\n"
                        print(f"  ✅ Page {page_num + 1}: extracted {len(page_text)} characters")
                    else:
                        print(f"  ⚠️ Page {page_num + 1}: no text found (might be image)")
                except Exception as e:
                    print(f"  ❌ Error on page {page_num + 1}: {str(e)}")
                    continue
            
            if not text.strip():
                raise Exception("No text could be extracted. PDF might be scanned images without OCR text layer.")
            
            return text
            
    except Exception as e:
        print(f"❌ PDF extraction failed: {str(e)}")
        raise

def extract_text_from_docx(file_path: str) -> str:
    """Extract text from DOCX files"""
    try:
        doc = Document(file_path)
        text = ""
        for paragraph in doc.paragraphs:
            text += paragraph.text + "\n"
        
        if not text.strip():
            raise Exception("No text found in DOCX file")
        
        print(f"📝 Extracted {len(text)} characters from DOCX")
        return text
        
    except Exception as e:
        print(f"❌ DOCX extraction failed: {str(e)}")
        raise

def extract_text_from_txt(file_path: str) -> str:
    """Extract text from TXT files"""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            text = file.read()
        
        if not text.strip():
            raise Exception("Text file is empty")
        
        print(f"📝 Extracted {len(text)} characters from TXT")
        return text
        
    except UnicodeDecodeError:
        # Try different encoding
        try:
            with open(file_path, 'r', encoding='latin-1') as file:
                text = file.read()
            print(f"📝 Extracted {len(text)} characters from TXT (latin-1 encoding)")
            return text
        except Exception as e:
            raise Exception(f"Cannot read text file: {str(e)}")
    except Exception as e:
        print(f"❌ TXT extraction failed: {str(e)}")
        raise

def chunk_text(text: str, chunk_size: int = 500) -> List[str]:
    """Split text into chunks for vector database"""
    words = text.split()
    chunks = []
    current_chunk = []
    current_length = 0
    
    for word in words:
        current_length += len(word) + 1
        if current_length > chunk_size:
            chunks.append(" ".join(current_chunk))
            current_chunk = [word]
            current_length = len(word)
        else:
            current_chunk.append(word)
    
    if current_chunk:
        chunks.append(" ".join(current_chunk))
    
    return chunks

# ========== API ENDPOINTS ==========
@app.get("/")
async def root():
    return {
        "status": "Ollama + Vector DB Server Running",
        "documents_count": collection.count(),
        "endpoints": [
            "POST /chat - Send message to AI",
            "POST /upload - Upload document to vector DB",
            "GET /documents - List all documents",
            "GET /search - Search documents"
        ]
    }

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    # Formulate location context if provided
    location_context = ""
    if request.latitude is not None and request.longitude is not None:
        location_context = f"[System Alert: The user's current GPS location coordinates are Latitude: {request.latitude}, Longitude: {request.longitude}. Use this info for spatial, distance, navigation, or location-specific questions if the user asks.]\n\n"

    if request.use_rag and collection.count() > 0:
        # Search vector database
        results = collection.query(
            query_texts=[request.message],
            n_results=2
        )
        
        contexts = []
        sources = []
        if results['documents'][0]:
            for doc, metadata in zip(results['documents'][0], results['metadatas'][0]):
                contexts.append(doc)
                sources.append(metadata.get('filename', 'unknown'))
        
        # Build prompt with context
        context_text = "\n\n".join(contexts)
        prompt = f"""{location_context}Context information:
{context_text}

Based on the context above, answer this question: {request.message}

If the context doesn't contain relevant information, say so and provide a general answer."""
        
        response = ollama.chat(
            model="deepseek-r1:7b",
            messages=[{"role": "user", "content": prompt}]
        )
        
        return ChatResponse(
            reply=response['message']['content'],
            sources=sources
        )
    else:
        # Regular chat
        prompt = f"{location_context}{request.message}"
        response = ollama.chat(
            model="deepseek-r1:7b",
            messages=[{"role": "user", "content": prompt}]
        )
        return ChatResponse(reply=response['message']['content'])

@app.post("/upload")
async def upload_document(file: UploadFile = File(...)):
    """Upload and process documents with improved error handling"""
    
    # Validate file type
    if not (file.filename.endswith('.pdf') or 
            file.filename.endswith('.txt') or 
            file.filename.endswith('.docx')):
        raise HTTPException(400, "Only PDF, TXT, and DOCX files allowed")
    
    temp_path = f"./documents/{file.filename}"
    
    try:
        # Read and save the uploaded file
        print(f"\n📤 Receiving file: {file.filename}")
        content = await file.read()
        file_size = len(content)
        print(f"📦 File size: {file_size} bytes ({file_size/1024:.1f} KB)")
        
        with open(temp_path, "wb") as buffer:
            buffer.write(content)
        print(f"💾 File saved to: {temp_path}")
        
        # Extract text based on file type
        print(f"🔍 Extracting text from {file.filename}...")
        
        if file.filename.endswith('.pdf'):
            text = extract_text_from_pdf(temp_path)
        elif file.filename.endswith('.docx'):
            text = extract_text_from_docx(temp_path)
        else:
            text = extract_text_from_txt(temp_path)
        
        # Check if text was extracted successfully
        if not text or not text.strip():
            raise HTTPException(
                400, 
                "No text could be extracted. The file might be empty or contain only images."
            )
        
        print(f"✅ Extracted {len(text)} characters of text")
        
        # Chunk the text
        chunks = chunk_text(text)
        print(f"🔨 Created {len(chunks)} chunks for vector database")
        
        if not chunks:
            raise HTTPException(400, "Text is too short to process")
        
        # Add chunks to vector database
        added_count = 0
        for i, chunk in enumerate(chunks):
            try:
                doc_id = f"{file.filename}_{uuid.uuid4().hex[:8]}"
                collection.add(
                    documents=[chunk],
                    metadatas=[{"filename": file.filename, "chunk": i}],
                    ids=[doc_id]
                )
                added_count += 1
            except Exception as e:
                print(f"⚠️ Error adding chunk {i}: {str(e)}")
                continue
        
        print(f"✅ Successfully added {added_count}/{len(chunks)} chunks to database")
        print(f"📊 Total documents in database: {collection.count()}")
        
        return {
            "message": f"Successfully uploaded {file.filename}",
            "chunks_added": added_count,
            "total_chunks": len(chunks),
            "total_documents_in_db": collection.count(),
            "text_length": len(text)
        }
    
    except HTTPException:
        raise
    except Exception as e:
        error_msg = f"Error processing {file.filename}: {str(e)}"
        print(f"❌ {error_msg}")
        raise HTTPException(500, error_msg)
    finally:
        # Clean up temporary file
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
                print(f"🗑️ Cleaned up temporary file: {temp_path}")
            except Exception as e:
                print(f"⚠️ Could not delete temp file: {str(e)}")

@app.get("/documents")
async def list_documents():
    if collection.count() == 0:
        return {"documents": [], "count": 0}
    
    results = collection.get()
    documents = {}
    
    for metadata in results['metadatas']:
        filename = metadata['filename']
        if filename not in documents:
            documents[filename] = {
                "filename": filename,
                "chunks": 0
            }
        documents[filename]["chunks"] += 1
    
    return {
        "documents": list(documents.values()),
        "total_chunks": collection.count()
    }

@app.get("/search")
async def search_documents(query: str, n_results: int = 3):
    if collection.count() == 0:
        return {"results": [], "message": "No documents in database"}
    
    results = collection.query(
        query_texts=[query],
        n_results=n_results
    )
    
    formatted_results = []
    if results['documents'][0]:
        for doc, metadata, distance in zip(
            results['documents'][0], 
            results['metadatas'][0],
            results['distances'][0]
        ):
            formatted_results.append({
                "preview": doc[:200] + "..." if len(doc) > 200 else doc,
                "filename": metadata['filename'],
                "relevance_score": round(1 - distance, 4)
            })
    
    return {"results": formatted_results}

# ========== SERVER STARTUP ==========
if __name__ == "__main__":
    import uvicorn
    print("=" * 50)
    print("🚀 STARTING FASTAPI SERVER WITH VECTOR DATABASE")
    print("=" * 50)
    print(f"📁 Documents folder: {os.path.abspath('./documents')}")
    print(f"💾 Vector DB folder: {os.path.abspath('./vector_db')}")
    print(f"📚 Documents in database: {collection.count()}")
    print("=" * 50)
    print("🌐 Server will start at: http://0.0.0.0:8000")
    print("📖 API Documentation: http://localhost:8000/docs")
    print("=" * 50)
    uvicorn.run(app, host="0.0.0.0", port=8000)