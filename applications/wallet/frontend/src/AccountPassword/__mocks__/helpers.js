/**
 * onSubmit
 */

export const onSubmit = ({ dispatch }) =>
  dispatch({
    currentPassword: 'foo',
    newPassword: 'bar',
    confirmPassword: 'bar',
  })
