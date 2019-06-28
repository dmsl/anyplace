import { environment } from '../environments/environment';
let url = environment.apiUrl;

export const AppConstant = Object.freeze({
    BASE_API_URL: url,
});