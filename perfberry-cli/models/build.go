package models

import (
	"encoding/json"
	"io/ioutil"
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/go-yaml/yaml"
)

type Build struct {
	ID          int      `json:"id,omitempty" yaml:"id,omitempty"`
	CreatedAt   string   `json:"created_at,omitempty" yaml:"created_at,omitempty"`
	Env         string   `json:"env" yaml:"env"`
	Label       string   `json:"label,omitempty" yaml:"label,omitempty"`
	Description string   `json:"description,omitempty" yaml:"description,omitempty"`
	Scm         *ScmInfo `json:"scm,omitempty" yaml:"scm,omitempty"`
	Links       []Link   `json:"links" yaml:"links"`
	Passed      *bool    `json:"passed,omitempty" yaml:"passed,omitempty"`
}

func NewBuildFromYAML(data []byte) (b *Build, err error) {
	err = yaml.Unmarshal(data, &b)
	return
}

func NewBuildFromFile(path string) (b *Build, err error) {
	data, err := helpers.ProccessTemplateFile(path)
	if err != nil {
		return
	}

	return NewBuildFromYAML(data)
}

func (b *Build) ToJSON() ([]byte, error) {
	return json.Marshal(*b)
}

func (b *Build) ToYAML() ([]byte, error) {
	return yaml.Marshal(*b)
}

func (b *Build) ToFile(path string) error {
	bytes, err := b.ToYAML()
	if err != nil {
		return err
	}

	filePath := "build-" + strconv.Itoa(b.ID) + ".yml"
	if path != "" {
		filePath = path
	}

	return ioutil.WriteFile(filePath, bytes, 0644)
}

func (b *Build) Path() string {
	var id string
	if b.ID == 0 {
		id = helpers.RandString()
	} else {
		id = strconv.Itoa(b.ID)
	}

	return "build-" + id + ".yml"
}
