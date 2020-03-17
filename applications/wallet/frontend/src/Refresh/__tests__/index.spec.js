import TestRenderer, { act } from 'react-test-renderer'

import Refresh from '..'

describe('<Refresh />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Refresh onClick={mockFn}>Refresh Stuff</Refresh>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root.findByProps({ variant: 'PRIMARY' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalled()
    expect(component.toJSON()).toMatchSnapshot()
  })
})
