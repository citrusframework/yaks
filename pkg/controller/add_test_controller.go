package controller

import (
	"github.com/citrusframework/yaks/pkg/controller/test"
)

func init() {
	// AddToManagerFuncs is a list of functions to create controllers and add them to a manager.
	AddToManagerFuncs = append(AddToManagerFuncs, test.Add)
}
