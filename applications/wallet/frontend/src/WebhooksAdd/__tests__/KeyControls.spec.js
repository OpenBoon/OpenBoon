import TestRenderer, { act } from 'react-test-renderer'

import WebhooksAddKeyControls from '../KeyControls'

describe('<WebhooksAddKeyControls />', () => {
  beforeAll(() => {
    jest.useFakeTimers()
  })

  afterAll(() => {
    jest.useRealTimers()
  })

  it('should render properly', async () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <WebhooksAddKeyControls
        state={{
          disableSecretKeyButton: false,
          isCopied: true,
          secretKey: 'some-random-key',
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // wait for timeout
    jest.runAllTimers()

    expect(mockDispatch).toHaveBeenCalledWith({ isCopied: false })

    act(() => {
      component.unmount()
    })
  })
})
