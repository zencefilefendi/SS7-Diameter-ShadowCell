package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/shadowcell/backend/internal/api"
	"github.com/shadowcell/backend/internal/db"
)

func main() {
	log.Println("ShadowCell Backend Starting (Advanced Prototype)...")

	// Initialize Database
	database, err := db.NewDatabase("./shadowcell_anomalies.db")
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}

	// Initialize API Router
	apiHandler := api.New(database)
	
	r := chi.NewRouter()
	
	// Middleware
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	// Timeout
	r.Use(middleware.Timeout(60 * time.Second))

	// Routes
	r.Mount("/api/v1", apiHandler.Routes())

	port := os.Getenv("PORT")
	if port == "" {
		port = "8443"
	}

	log.Printf("Server listening on :%s", port)
	// Note: In production, use ListenAndServeTLS with proper certificates for mTLS.
	if err := http.ListenAndServe(":"+port, r); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}
