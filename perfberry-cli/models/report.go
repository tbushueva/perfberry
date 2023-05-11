package models

import (
	"encoding/json"
	"io/ioutil"
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/go-yaml/yaml"
)

type Report struct {
	ID          int      `json:"id,omitempty" yaml:"id,omitempty"`
	CreatedAt   string   `json:"created_at,omitempty" yaml:"created_at,omitempty"`
	Label       string   `json:"label,omitempty" yaml:"label,omitempty"`
	Description string   `json:"description,omitempty" yaml:"description,omitempty"`
	Scm         *ScmInfo `json:"scm,omitempty" yaml:"scm,omitempty"`
	Links       []Link   `json:"links" yaml:"links"`
	Passed      *bool    `json:"passed,omitempty" yaml:"passed,omitempty"`
}

func NewReportFromJSON(data []byte) (r *Report, err error) {
	err = json.Unmarshal(data, &r)
	return
}

func NewReportFromYAML(data []byte) (r *Report, err error) {
	err = yaml.Unmarshal(data, &r)
	return
}

func NewReportFromFile(path string) (r *Report, err error) {
	data, err := helpers.ProccessTemplateFile(path)
	if err != nil {
		return
	}

	return NewReportFromYAML(data)
}

func (r *Report) ToJSON() ([]byte, error) {
	return json.Marshal(*r)
}

func (r *Report) ToYAML() ([]byte, error) {
	return yaml.Marshal(*r)
}

func (r *Report) ToFile(path string) error {
	bytes, err := r.ToYAML()
	if err != nil {
		return err
	}

	return ioutil.WriteFile(path, bytes, 0644)
}

func (r *Report) Path() string {
	var id string
	if r.ID == 0 {
		id = helpers.RandString()
	} else {
		id = strconv.Itoa(r.ID)
	}

	return "report-" + id + ".yml"
}
