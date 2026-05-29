package db

import (
	"database/sql"
	"log"
	"time"

	_ "github.com/mattn/go-sqlite3"
)

type AnomalyEvent struct {
	ID             int64
	HashedCellID   string
	EventType      string
	RiskScore      int
	Timestamp      int64
	ReportedAt     int64
	CountryCode    string
}

type Database struct {
	db *sql.DB
}

func NewDatabase(dataSourceName string) (*Database, error) {
	db, err := sql.Open("sqlite3", dataSourceName)
	if err != nil {
		return nil, err
    }

	// Create tables if not exist
	query := `
	CREATE TABLE IF NOT EXISTS anomalies (
		id INTEGER PRIMARY KEY AUTOINCREMENT,
		hashed_cell_id TEXT NOT NULL,
		event_type TEXT NOT NULL,
		risk_score INTEGER NOT NULL,
		timestamp INTEGER NOT NULL,
		reported_at INTEGER NOT NULL,
		country_code TEXT NOT NULL
	);
	CREATE INDEX IF NOT EXISTS idx_hashed_cell_id ON anomalies(hashed_cell_id);
	CREATE INDEX IF NOT EXISTS idx_timestamp ON anomalies(timestamp);
	`
	_, err = db.Exec(query)
	if err != nil {
		return nil, err
	}

	return &Database{db: db}, nil
}

func (d *Database) InsertAnomaly(event AnomalyEvent) error {
	query := `INSERT INTO anomalies (hashed_cell_id, event_type, risk_score, timestamp, reported_at, country_code) 
			  VALUES (?, ?, ?, ?, ?, ?)`
	_, err := d.db.Exec(query, event.HashedCellID, event.EventType, event.RiskScore, event.Timestamp, time.Now().Unix(), event.CountryCode)
	return err
}

func (d *Database) GetRecentAnomaliesByCell(hashedCellID string, since int64) ([]AnomalyEvent, error) {
	query := `SELECT id, hashed_cell_id, event_type, risk_score, timestamp, reported_at, country_code 
			  FROM anomalies WHERE hashed_cell_id = ? AND timestamp > ? ORDER BY timestamp DESC`
	rows, err := d.db.Query(query, hashedCellID, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []AnomalyEvent
	for rows.Next() {
		var e AnomalyEvent
		if err := rows.Scan(&e.ID, &e.HashedCellID, &e.EventType, &e.RiskScore, &e.Timestamp, &e.ReportedAt, &e.CountryCode); err != nil {
			log.Println("Error scanning row:", err)
			continue
		}
		events = append(events, e)
	}
	return events, nil
}
