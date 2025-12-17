# apt.
### Description:
A mobile application to serve as a hub for all roommate related needs such as transactions, anonymous complaints, chores, and more!

### Build/Run instructions: 
- Clone repo
- If you want photo upload, create a supabase project with storage bucket named chore_photos. Get the API key and link for the project and put them in your local.properties file.
- Start emulator
- Build and run app

### Team:
- Alice Han (Tech Lead)
- Tiffany Liu (Scrum Master)
- Wyatt Napier (Product Owner)

### Feature List 
- User auth with Google Sign-in API
- User login and register flows including ousehold creation and joining pages
- Basic home page with widgets that show snapshots of other core pages. Users can select which widgets they want to display
- Chores page that allows users to upload photos from their camera as evidence of chore completion
- Events page that allows users to view events as an agenda, 3 day view, or month view. It also lets users add new events to their shared calendar
- Payment page which routes users to venmo to complete transactions related to their household
- Logic for assigning chores and payments on a recurring basis
- Unit testing, UI testing, and testing for various on devices

### Testing Strategy
- handling errors with try-catch blocks and when necessary displaying error with a Toast
- comprehensive logging throughout complex processes such as API calls and retrieval of data from database
- reading through stack trace following crashes
- basic compose UI testing on the sign-in/registration flow
- manually testing UI on different device sizes

### Team Workflow
- Used GitHub Projects to create issues and assign as we go based on priority
- Different branches for different features to streamline collaboration
- Pull requests for code review
- Overall has been very successful with no merge conflicts!

### Documentation and Diagrams
<img width="873" height="491" alt="Screenshot 2025-12-02 at 7 23 42 PM" src="https://github.com/user-attachments/assets/bdf57ae2-40c0-43a5-8247-701053d99bf8" />

Model Layer  
- We primarily store our data in Google Firebase and we fetch the data through the single FirestoreRepository, which allows us to have a more centralized way of accessing the data.
- Firebase is also used for authentication, which is connected directly to the login ViewModel
- We call the Google Calendar API, which is only accessed in events page, so the calls are made directly through that ViewModel
- We use a separate Supabase database to store images, which is accessed onlyin the chores page, so the calls are made through that ViewModel
  
ViewModel Layer
- Each of the pages has a separate ViewModel which takes care of the logic and data parsing needed for that page

View Layer
- Each view takes in the state from the ViewModel and displays the reactive UI for the users
<img width="873" height="491" alt="Screenshot 2025-12-02 at 3 51 17 PM" src="https://github.com/user-attachments/assets/7bc0d73e-6293-4ef4-8941-86456c58a16e" />

<img width="189" height="525" alt="PNG image" src="https://github.com/user-attachments/assets/533fa4ef-8b87-4300-a32b-28ec0c70c682" />

### AI Usage Statement
- Primarily used Google Gemini and Claude
- Provided relevant context and queried for explanations on logical errors in code
- Used to debug crashes by providing the stack trace
- Met some inconsistencies in coding styles between the major tools which lead to some compatibility issues and required refinement
- Struggled with build errors due to dependencies incompatibilities
- Unable to come up with alternative solutions to more complex errors such as Google Sign-in Error 7: Network Error
- Required correction when overriding functions that would then trigger updates across the entire project
- Used it to help come up with both UI and unit tests, but it struggled a lot with dependencies for viewModels and understanding how to mock repository activity.

### Future Improvements
Some additional features that we could add in the future if we continue to work on the project: 
- Adding notifications to remind users about upcoming deadlines 
- Expanding household and user settings
- Anonymous note board
