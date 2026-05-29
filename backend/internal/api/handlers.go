package api

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/shadowcell/backend/internal/crypto"
	"github.com/shadowcell/backend/internal/db"
)

type API struct {
	db *db.Database
}

func New(database *db.Database) *API {
	return &API{db: database}
}

func (a *API) Routes() chi.Router {
	r := chi.NewRouter()
	r.Post("/report", a.HandleReport)
	r.Get("/check/{mcc}/{mnc}/{cellID}", a.HandleCheckCell)
	return r
}

type ReportRequest struct {
	MCC         string `json:"mcc"`
	MNC         string `json:"mnc"`
	CellID      string `json:"cellId"`
	EventType   string `json:"eventType"`
	RiskScore   int    `json:"riskScore"`
	Timestamp   int64  `json:"timestamp"`
	CountryCode string `json:"countryCode"`
}

func (a *API) HandleReport(w http.ResponseWriter, r *http.Request) {
	var req ReportRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	hashedCell := crypto.HashCellID(req.MCC, req.MNC, req.CellID)

	event := db.AnomalyEvent{
		HashedCellID: hashedCell,
		EventType:    req.EventType,
		RiskScore:    req.RiskScore,
		Timestamp:    req.Timestamp,
		CountryCode:  req.CountryCode,
	}

	if err := a.db.InsertAnomaly(event); err != nil {
		http.Error(w, "Failed to store anomaly", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusAccepted)
}

func (a *API) HandleCheckCell(w http.ResponseWriter, r *http.Request) {
	mcc := chi.URLParam(r, "mcc")
	mnc := chi.URLParam(r, "mnc")
	cellID := chi.URLParam(r, "cellID")

	hashedCell := crypto.HashCellID(mcc, mnc, cellID)
	
	// Check anomalies in the last 24 hours
	since := time.Now().Add(-24 * time.Hour).Unix()
	
	events, err := a.db.GetRecentAnomaliesByCell(hashedCell, since)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"hashed_cell": hashedCell,
		"anomalies":   events,
		"count":       len(events),
	})
}
