const Error = () => {
  return (
    <div className="h-full flex flex-col justify-center items-center text-gray-600">
      <svg className="w-64" viewBox="0 0 604 201">
        <path
          d="M330.322 163.688c-.755-1.574-2.62-2.135-4.337-1.308-1.716.828-2.442 2.639-1.688 4.211l11.376 23.72c.472.985 1.37 1.596 2.464 1.68.084.006.168.009.252.009 1.194 0 2.406-.628 3.078-1.615.619-.908.703-1.993.231-2.976l-11.376-23.721zm21.952-13.871a2.72 2.72 0 00-2.741-.7c-1.165.338-2.146 1.371-2.441 2.57-.254 1.031.025 2.044.766 2.78l17.873 17.722a2.732 2.732 0 001.943.811c.822 0 1.668-.362 2.344-1.075 1.259-1.325 1.314-3.21.129-4.386l-17.873-17.722zm-74.489 16.319c-1.615-.483-3.14.358-3.548 1.958l-6.138 24.119c-.255 1-.013 2 .664 2.745A3.228 3.228 0 00271.12 196c.262 0 .523-.034.775-.105.933-.263 1.615-.988 1.87-1.988l6.138-24.12c.407-1.598-.503-3.168-2.118-3.651m28.242 5.548c-.196-1.72-1.85-2.863-3.848-2.661-1.997.202-3.356 1.65-3.16 3.37l2.954 25.922c.122 1.075.799 1.945 1.856 2.386.482.2 1.015.299 1.55.299.829 0 1.663-.236 2.324-.69.935-.643 1.4-1.628 1.278-2.703l-2.954-25.923zM267.653 42.149c.463 1.172 1.563 1.851 2.813 1.851.44 0 .9-.085 1.357-.262 1.75-.68 2.598-2.388 1.972-3.973L264.35 15.85c-.392-.992-1.23-1.653-2.3-1.814-1.245-.187-2.61.342-3.395 1.318-.676.838-.838 1.888-.446 2.88l9.443 23.914zM247.73 57.188a2.733 2.733 0 001.944.812c.821 0 1.667-.362 2.343-1.074 1.26-1.326 1.315-3.212.13-4.388L234.27 34.811c-1.186-1.175-3.03-1.061-4.288.264-1.259 1.326-1.314 3.212-.128 4.388l17.876 17.725zm73.427-17.333c.31.098.619.145.916.145 1.219 0 2.258-.788 2.59-2.073l6.233-24.104c.259-1 .029-2.004-.631-2.756-.768-.876-2.004-1.265-3.075-.969-.92.255-1.597.974-1.856 1.974l-6.233 24.105c-.413 1.598.47 3.18 2.056 3.678m-26.69-2.857c2.063-.05 3.582-1.385 3.532-3.107l-.759-25.957c-.03-1.077-.647-1.99-1.692-2.508-1.215-.602-2.824-.563-4.002.097-1.013.567-1.576 1.51-1.545 2.587l.758 25.957c.05 1.689 1.583 2.933 3.59 2.933.038 0 .078 0 .118-.002"
          fill="#4A4A4A"
        />
        <path
          d="M239.248 141.453c-12.884 3.038-25.834-4.982-28.87-17.878l-.475-2.02c-3.035-12.897 4.977-25.86 17.86-28.898l21.653-5.106 11.484 48.796-21.652 5.106zm18.272-67.015l-32.254 7.606c-17.05 4.021-28.23 20-26.77 36.975a874.77 874.77 0 00-8.794 2.004c-13.913 3.222-22.312 5.167-34.304 3.583-7.942-1.048-15.695-3.619-22.421-7.433-7.172-4.067-12.998-9.366-17.318-15.748-2.319-3.426-4.226-7.172-6.246-11.138-1.921-3.771-3.908-7.671-6.318-11.367-9.642-14.784-25.83-25.446-44.413-29.252-8.289-1.698-17.12-2.102-26.248-1.2-3.028.298-6.096.742-9.19 1.326l.004-.006a107.572 107.572 0 00-13.066 3.26l-.084.027-.74.233c-.81.257-1.621.52-2.433.794l.002.009c-1.406.481-2.045.737-2.045.737C1.254 56.305-.808 59.597.3 62.162c1.108 2.566 4.985 3.478 8.615 2.027 0 0 .402-.16 1.26-.46v.002c16.65-5.63 32.285-7.012 46.472-4.106 15.946 3.266 29.777 12.324 37.947 24.851 2.113 3.24 3.891 6.732 5.774 10.428 2.071 4.067 4.214 8.274 6.885 12.221 5.205 7.69 12.176 14.046 20.72 18.89 7.85 4.453 16.874 7.45 26.098 8.668 3.42.451 6.58.655 9.62.655 9.244 0 17.378-1.884 28.301-4.413 2.64-.612 5.501-1.274 8.6-1.96 5.576 17.036 23.408 27.285 41.155 23.1L274 144.46l-16.48-70.02zm118.09 34.365l-21.678 5.1-11.498-48.738 21.678-5.1c12.899-3.034 25.865 4.976 28.903 17.857l.477 2.018c3.038 12.88-4.983 25.828-17.882 28.863m227.323-75.719c-1.98-1.937-6.168-1.701-9.347.511-13.765 7.408-31.376-2.834-40.65-9.599a435.978 435.978 0 01-6.009-4.492C534.89 10.398 522.444.981 506.843.093c-8.175-.466-16.607.833-24.384 3.755a59.443 59.443 0 00-21.961 14.405c-6.27 6.476-10.461 13.093-14.514 19.493-2.913 4.599-5.925 9.354-9.726 14.07-10.722 13.302-14.182 14.056-30.81 17.676-1.087.237-2.247.49-3.47.759-6.373-15.634-23.41-24.775-40.367-20.786l-32.293 7.597 2.809 11.904-30.794 7.244c-3.01.708-4.893 3.747-4.183 6.753l.09.385c.71 3.005 3.752 4.885 6.762 4.177l30.794-7.244 5.544 23.5-30.794 7.244c-3.01.708-4.892 3.747-4.183 6.753l.09.385c.71 3.005 3.753 4.885 6.763 4.177l30.793-7.244L345.818 127l32.293-7.597c17.876-4.206 29.309-21.509 26.51-39.339 1.047-.23 2.049-.448 2.993-.654 8.815-1.919 14.638-3.187 20.077-6.08 5.389-2.868 10.011-7.115 16.486-15.148 4.164-5.167 7.334-10.17 10.398-15.01 3.954-6.24 7.688-12.136 13.232-17.863a49.354 49.354 0 0118.234-11.96c6.459-2.428 13.453-3.507 20.223-3.121 12.509.712 23.701 9.18 34.525 17.368 2.011 1.522 4.091 3.095 6.151 4.598 8.288 6.045 16.428 10.219 24.196 12.404 7.63 2.147 14.682 2.3 21.05.47l.002.004c5.646-1.622 8.77-4.06 8.77-4.06 3.083-2.407 3.972-5.974 1.975-7.928"
          fill="#808080"
        />
      </svg>
      <br />
      Hmm, something went wrong.
      <br />
      Please try refreshing.
    </div>
  )
}

export default Error