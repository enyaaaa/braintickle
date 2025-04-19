# Braintickle
<img src="https://github.com/enyaaaa/braintickle/blob/main/logoicon.png?raw=true" width="200" height="200">

Braintickle is an interactive quiz application that provides engaging, educational trivia challenges for users to test their knowledge across various categories.
This project was done for IM2073 - Intro to Design & Project (mobile programming module).

# features

- <b>Quiz</b>
  - Start quiz by category
  - Join session
    - Session id
    - Player name
  - View Quesiton
    - Show question with 4 options and a timer synced up with both web and mobile
    - Feedback message correct, incorrect or time's up
    - Next question button appear when timer is up
  - Leaderboard
    - Final score will be shown after answers 5 question of that category
  
# Technologies used

- <b>Frontend</b>
  web
    - html
    - css
    - javascript
 
  mobile
  - android studio (java)
  
- <b>backend</b>
  -  apache tomcat (hosting)
  -  sql (database)

# Instuctions
install sql and tomcat

- <b>update servlets</b>
  - classes - open in integrated terminal
  - javac -cp ../../../../lib/servlet-api.jar *.java

- <b>run program</b>
  - cd ~/myWebProject/tomcat/
  - ./catalina.sh run
