package report

import (
	"os"
	"path"
)

func getInWorkingDir(dir string) (string, error) {
	workingDir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	outputDir := path.Join(workingDir, dir)
	_, err = os.Stat(outputDir)

	return outputDir, err
}

func createInWorkingDir(dir string) (string, error) {
	workingDir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	newDir := path.Join(workingDir, dir)
	err = createIfNotExists(newDir)
	return newDir, err
}

func removeFromWorkingDir(dir string) error {
	workingDir, err := os.Getwd()
	if err != nil {
		return err
	}

	toDelete := path.Join(workingDir, dir)
	if _, err := os.Stat(dir); err == nil {
		err = os.RemoveAll(toDelete)
		if err != nil {
			return err
		}
	} else if !os.IsNotExist(err) {
		return err
	}

	return nil
}

func createIfNotExists(dir string) error {
	if _, err := os.Stat(dir); err != nil && os.IsNotExist(err) {
		if err := os.Mkdir(dir, 0755); err != nil {
			return err
		}
	}

	return nil
}
