package models

import (
	"encoding/json"
)

type Project struct {
	ID    int    `json:"id,omitempty"`
	Alias string `json:"alias"`
	Name  string `json:"name"`
}

func NewProjectFromJSON(data []byte) (p *Project, err error) {
	err = json.Unmarshal(data, &p)
	return
}
