package models

import (
	"encoding/json"
	"io/ioutil"

	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/go-yaml/yaml"
)

type Assertion struct {
	Group     string  `json:"group,omitempty" yaml:"group,omitempty"`
	Metric    string  `json:"metric" yaml:"metric"`
	Selector  string  `json:"selector,omitempty" yaml:"selector,omitempty"`
	Condition string  `json:"condition" yaml:"condition"`
	Expected  float32 `json:"expected" yaml:"expected"`
}

type Assertions []Assertion

func NewAssertionsFromYAML(data []byte) (a *Assertions, err error) {
	err = yaml.Unmarshal(data, &a)
	return
}

func NewAssertionsFromFile(path string) (a *Assertions, err error) {
	data, err := helpers.ProccessTemplateFile(path)
	if err != nil {
		return
	}

	return NewAssertionsFromYAML(data)
}

func (a *Assertions) ToJSON() ([]byte, error) {
	return json.Marshal(*a)
}

func (a *Assertions) ToYAML() ([]byte, error) {
	return yaml.Marshal(*a)
}

func (a *Assertions) ToFile(path string) error {
	bytes, err := a.ToYAML()
	if err != nil {
		return err
	}

	filePath := a.Path()
	if path != "" {
		filePath = path
	}

	return ioutil.WriteFile(filePath, bytes, 0644)
}

func (a *Assertions) Path() string {
	return "assertions-" + helpers.RandString() + ".yml"
}
