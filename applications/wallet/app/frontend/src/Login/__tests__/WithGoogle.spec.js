import TestRenderer, { act } from 'react-test-renderer'

import LoginWithGoogle from '../WithGoogle'

describe('<LoginWithGoogle />', () => {
  it('should render properly', async () => {
    const mockSignIn = jest.fn(() =>
      Promise.resolve({ Zi: { id_token: 'ID_TOKEN' } }),
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

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockSignIn).toHaveBeenCalledWith()

    expect(mockOnSubmit).toHaveBeenCalledWith({ idToken: 'ID_TOKEN' })
  })
})
