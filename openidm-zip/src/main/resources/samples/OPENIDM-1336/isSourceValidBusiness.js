function isSourceValid(sourceobject)
{
    if (sourceobject.accounts != null) {
        for (i = 0; i < sourceobject.accounts.length; i++) {
            if ("Business" == sourceobject.accounts[i]) {
                return true;
            }
        }
    }
    return false;
}

isSourceValid(source);

