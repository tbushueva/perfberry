package helpers

import (
	"os"
	"path/filepath"
	"regexp"
)

func SearchDirs(dir string) ([]string, error) {
	matched, err := filepath.Glob(dir)
	if err != nil {
		return nil, err
	}

	return matched, nil
}

func SearchFiles(dir string, fileName string) ([]string, error) {
	dirs, err := SearchDirs(dir)
	if err != nil {
		return nil, err
	}

	r, _ := regexp.Compile(fileName)
	var list []string
	for _, d := range dirs {
		err := filepath.Walk(d, func(path string, f os.FileInfo, err error) error {
			if err != nil {
				return err
			}

			if r.MatchString(f.Name()) {
				list = append(list, path)
			}
			return nil
		})
		if err != nil {
			return nil, err
		}
	}

	return list, nil
}
