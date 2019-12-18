import TestRenderer, { act } from 'react-test-renderer'

import LoginWithGoogle from '../WithGoogle'

describe('<LoginWithGoogle />', () => {
  it('should render properly', async () => {
    const mockGetAuthResponse = jest.fn(() => ({ id_token: 'ID_TOKEN' }))
    const mockSignIn = jest.fn(() =>
      Promise.resolve({ getAuthResponse: mockGetAuthResponse }),
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
        .findByProps({ children: 'Sign In with Google' })
        .props.onClick()
    })

    expect(mockSignIn).toHaveBeenCalledWith()

    expect(mockOnSubmit).toHaveBeenCalledWith({ idToken: 'ID_TOKEN' })
  })
})
