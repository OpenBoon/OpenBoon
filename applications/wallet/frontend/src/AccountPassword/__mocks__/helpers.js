export const onSubmit = ({ dispatch }) =>
  dispatch({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    success: true,
  })
