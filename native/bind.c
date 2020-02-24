#include <errno.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <sys/un.h>
#include <unistd.h>
#include <netdb.h>

#ifdef ANDROID
#include <android/log.h>
#define LOG_TAG "PortBinder"
#define LOGI(...) android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS)
#define LOGE(...) android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS)
#else
#define LOGI printf
#define LOGE printf
#endif

#define CONTROLLEN CMSG_LEN(sizeof(int))

// LocalSocket uses the Linux abstract namespace instead of the filesystem.
// In C these addresses are specified by prepending '\0' to the path.
// http://stackoverflow.com/questions/14643571/localsocket-communication-with-unix-domain-in-android-ndk
#define UNIX_PATH "\0hostage"

#define TCP "TCP"
#define UDP "UDP"

// create unix domain local socket for inter process communication
int ipc_sock() {
	int fd;
	struct sockaddr_un addr;

	if ((fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
		perror("Unable to create local socket");
		return -1;
	}

	// Also note that you should not pass sizeof(sockaddr_un) to bind or connect because all bytes
	// following the '\0' character are interpreted as the abstract socket name. Calculate and pass
	// the real size instead
	addr.sun_family = AF_UNIX;
	int addrsize = strlen(&(UNIX_PATH)[1]) + 1;
	memcpy(addr.sun_path, UNIX_PATH, addrsize);

	addrsize += sizeof(addr.sun_family); // total size
	if (connect(fd, (struct sockaddr*)&addr, addrsize) == -1) {
		perror("Unable to connect local socket");
		return -1;
	}

	return fd;
}

int net_sock(int type, int port) {
	int fd;
	int reuseaddr = 1;

	struct addrinfo hints, *res;

	// first, load up address structs with getaddrinfo():
	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_INET; // use IPv4
	hints.ai_socktype = (type == 1 ? SOCK_STREAM : SOCK_DGRAM); // TCP or UDP
	hints.ai_flags = AI_PASSIVE; // fill in my IP for me

	char service_name[256];
	sprintf(service_name, "%d", port);
	getaddrinfo(NULL, service_name, &hints, &res);

	if ((fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) == -1) {
		perror("Unable to create net socket");
		return -1;
	}

	if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuseaddr, sizeof(reuseaddr)) == -1) {
		perror("Unable to set socket options");
		return -1;
	}

	if (bind(fd, res->ai_addr, res->ai_addrlen) == -1) {
		perror("Unable to bind net socket");
		return -1;
	}

	if (type == 1) { // TCP
		if (listen(fd, 5) == -1) {
			perror("Unable to listen net socket");
			return -1;
		}
	}

	return fd;
}

// send a file descriptor via inter process communication
int send_fd(int fd, int fd_to_send) {
	struct iovec iov[1];
	struct cmsghdr *cmptr;
	struct msghdr msg;
	char buf[] = "FD";

	iov[0].iov_base = buf;
	iov[0].iov_len = 2;

	cmptr = (struct cmsghdr *)malloc(CONTROLLEN);
	cmptr->cmsg_level = SOL_SOCKET;
	cmptr->cmsg_type = SCM_RIGHTS;
	cmptr->cmsg_len = CONTROLLEN;

	msg.msg_iov = iov;
	msg.msg_iovlen = 1;
	msg.msg_name = NULL;
	msg.msg_namelen = 0;
	msg.msg_control = cmptr;
	msg.msg_controllen = CONTROLLEN;
	*(int *) CMSG_DATA(cmptr) = fd_to_send;

	if (sendmsg(fd, &msg, 0) == -1) {
		perror("sendmsg failed");
	}

	return 0;
}

int main(int argc, char *argv[]) {
	int type;
	int port;
	int ipc_fd, net_fd;

	if (argc < 3) {
		LOGI("usage: %s <protocol> <port>\n", argv[0]);
		LOGI("where protocol is either TCP or UDP and port is between 1 and 65535\n");
		exit(EXIT_FAILURE);
	}

	LOGI("port binder reporting %s %s\n", argv[1], argv[2]);

	if (strncmp(argv[1], TCP, 3) == 0) {
		type = 1;
	} else if (strncmp(argv[1], UDP, 3) == 0) {
		type = 0;
	} else {
		exit(EXIT_FAILURE);
	}

	port = atoi(argv[2]);
	if (!(port >= 0 && port <= 65535)) {
		exit(EXIT_FAILURE);
	}

	if ((net_fd = net_sock(type, port)) == -1) {
		close(net_fd);
		exit(EXIT_FAILURE);
	}
	LOGI("net_fd: %d\n", net_fd);

	if ((ipc_fd = ipc_sock()) == -1) {
		close(net_fd);
		close(ipc_fd);
		exit(EXIT_FAILURE);
	}
	LOGI("ipc_fd: %d\n", ipc_fd);

	int status;
	status = send_fd(ipc_fd, net_fd);
	LOGI("send_fd: %d\n", status);

	close(ipc_fd);
	//close(net_fd);

	if (status == -1) {
		return (EXIT_FAILURE);
	}

	return EXIT_SUCCESS;
}
