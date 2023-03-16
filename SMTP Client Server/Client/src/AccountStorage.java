import java.util.ArrayList;

public class AccountStorage
{
    private ArrayList<Account> accounts; //List to store the accounts

    public AccountStorage()
    {
        accounts = new ArrayList<>(); //Initialize the list of accounts
    }

    public void addAccount(String username, String password)
    {
        //Create a new account object with the given username and password
        Account newAccount = new Account(username, password);
        //Add the new account to the list of accounts
        accounts.add(newAccount);
    }

    public boolean checkCredentials(String username, String password)
    {
        //Search for an account with the given username
        for (Account account : accounts)
        {
            if (account.getUsername().equals(username))
            {
                //If the username matches, check if the password also matches
                if (account.getPassword().equals(password))
                {
                    //If both the username and password match, return true
                    return true;
                } else
                {
                    //If the password doesn't match, return false
                    return false;
                }
            }
        }
        //If no account with the given username is found, return false
        return false;
    }
}
