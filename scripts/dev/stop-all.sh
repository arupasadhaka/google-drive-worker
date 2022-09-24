# todo graceful shut-down
sudo kill -9 $(lsof -t -i :80);
sudo kill -9 $(lsof -t -i :8000);
# devtool
sudo kill -9 $(lsof -t -i :35730);