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

## UPDATE: December 2, 2025
### Feature List
- [DONE] User auth with Google Sign-in API
- [DONE] User login and register flows including ousehold creation and joining pages
- [DONE] Basic home page with widgets that show snapshots of other core pages. Users can select which widgets they want to display
- [DONE] Chores page that allows users to upload photos from their camera as evidence of chore completion
- [DONE] Events page that allows users to view events as an agenda, 3 day view, or month view. It also lets users add new events to their shared calendar
- [DONE] Payment page which routes users to venmo to complete transactions related to their household
- [WIP] Logic for assigning chores and payments on a recurring basis
- [WIP] Extending unit testing and testing for various on devices
- [WIP] Adding notifications to app
- [WIP] Expanding household and user settings
- [STRETCH] Anonymous note board

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
<img width="873" height="491" alt="Screenshot 2025-12-02 at 3 51 17 PM" src="https://github.com/user-attachments/assets/7bc0d73e-6293-4ef4-8941-86456c58a16e" />
<img width="873" height="491" alt="Screenshot 2025-12-02 at 3 53 51 PM" src="https://github.com/user-attachments/assets/6bdce3a0-225a-4915-817a-aebaafa14f79" />
<img width="189" height="525" alt="PNG image" src="https://github.com/user-attachments/assets/533fa4ef-8b87-4300-a32b-28ec0c70c682" />

### AI Usage Statement
- Primarily used Google Gemini and Claude
- Provided relevant context and queried for explanations on logical errors in code
- Used to debug crashes by providing the stack trace
- Met some inconsistencies in coding styles between the major tools which lead to some compatibility issues and required refinement
- Struggled with build errors due to dependencies incompatibilities
- Unable to come up with alternative solutions to more complex errors such as Google Sign-in Error 7: Network Error
- Required correction when overriding functions that would then trigger updates across the entire project

## UPDATE: November 11, 2025 
### Current Features: 
- Bottom Nav Bar that allows users to visit each separate page
- Home screen with temporary widgets that navigate to each page
- Chores page with widgets showing own chore, roommate chores, and option to pull up all past chores
- Payment page with connection to Venmo API and preliminary logic to calculate payments
- ViewModel with mock data and preliminary logic for pages, will continue to build out as we add functionality to the app
- NavGraph that properly navigates through all pages in the main activity of the app
- Persistent DataStore to keep track of user preferences 
### Updates to project: 
- From considerations of real-world usage, we are no longer building out the locate page as part of our MVP.
