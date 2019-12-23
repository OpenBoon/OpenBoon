import TestRenderer, { act } from 'react-test-renderer'

import LoginWithGoogle from '../WithGoogle'

describe('<LoginWithGoogle />', () => {
  it('should render properly', async () => {
    const mockSignIn = jest.fn(() =>
      Promise.resolve({
        getAuthResponse: () => ({ id_token: 'ID_TOKEN' }),
      }),
    )
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <LoginWithGoogle
        googleAuth={{ signIn: mockSignIn }}
        hasGoogleLoaded
        onSubmit={mockOnSubmit}
      />,
    )

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Sign in with Google' })
        .props.onClick()
    })

    expect(mockSignIn).toHaveBeenCalledWith()

    expect(mockOnSubmit).toHaveBeenCalledWith({ idToken: 'ID_TOKEN' })
  })

  it('should recover from an error', async () => {
    const mockSignIn = jest.fn(() =>
      Promise.reject(new Error('popup_closed_by_user')),
    )
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <LoginWithGoogle
        googleAuth={{ signIn: mockSignIn }}
        hasGoogleLoaded
        onSubmit={mockOnSubmit}
      />,
    )

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Sign in with Google' })
        .props.onClick()
    })

    expect(mockSignIn).toHaveBeenCalledWith()

    expect(mockOnSubmit).not.toHaveBeenCalled()

    expect(component.toJSON()).toMatchSnapshot()
  })
})
