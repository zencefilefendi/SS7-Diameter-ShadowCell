package crypto

import (
	"crypto/sha256"
	"encoding/hex"
	"os"
)

var globalPepper = "shadowcell_military_grade_pepper_2026"

func init() {
	if envPepper := os.Getenv("SHADOWCELL_PEPPER"); envPepper != "" {
		globalPepper = envPepper
	}
}

// HashCellID anonymizes the Cell Global Identity (CGI) or physical cell ID.
// Using SHA-256 with a pepper to prevent rainbow table attacks.
// We do not store raw Cell IDs to protect user location privacy.
func HashCellID(mcc, mnc, cellID string) string {
	data := mcc + ":" + mnc + ":" + cellID + ":" + globalPepper
	hash := sha256.Sum256([]byte(data))
	return hex.EncodeToString(hash[:])
}
