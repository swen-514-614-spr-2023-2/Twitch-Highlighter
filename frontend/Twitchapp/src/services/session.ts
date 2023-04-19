import { logIn, register } from "./api-client";

export const logInUser = (name: any, password: any, isUserName: Boolean): any =>{
    let responseApi = logIn(name, password, isUserName);
    responseApi.then((response) => {
        debugger;
        if(response.has("user_id")){
            localStorage.setItem("isLogIn", "true");
            localStorage.setItem("userid", response.user_id);
            localStorage.setItem("useremail", response.email);
            localStorage.setItem("username", response.user_name);
        }else{
            localStorage.setItem("isLogIn", "false");
            localStorage.removeItem("userid");
            localStorage.removeItem("useremail");
            localStorage.removeItem("username");
        }
        return getIsUserLoggedIn();
    }).catch((err) => {
        debugger;
            localStorage.setItem("isLogIn", "false");
            localStorage.removeItem("userid");
            localStorage.removeItem("useremail");
            localStorage.removeItem("username");
            return getIsUserLoggedIn();
      });
}

export const registerUser = async (name: any, password: any, email: any) =>{
    let responseApi = await register(name, password, email).then((response) => {
        debugger;
        return response;
    }).catch((err) => {
        debugger;
        return false;
      });
}

export const getIsUserLoggedIn = () => {
    return localStorage.getItem("isLogIn") != null && Boolean(localStorage.getItem("isLogIn") == "true");
}

export const getIsUserId = () => {
    return localStorage.getItem("userid") != null && localStorage.getItem("userid");
}

export const getIsUserEmail = () => {
    return localStorage.getItem("useremail") != null && localStorage.getItem("useremail");
}

export const getIsUserName = () => {
    return localStorage.getItem("username") != null && localStorage.getItem("username");
}