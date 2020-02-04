/**
 * onSubmit
 */

export const onSubmit = ({ dispatch }) =>
  dispatch({
    firstName: 'Jane',
    lastName: 'Doe',
    showForm: false,
    success: true,
    errors: {},
  })
