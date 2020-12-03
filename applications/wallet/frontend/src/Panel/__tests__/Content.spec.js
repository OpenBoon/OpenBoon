import TestRenderer, { act } from 'react-test-renderer'

import PanelContent from '../Content'

const noop = () => {}

describe('<PanelContent />', () => {
  it('should render properly to the right', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <PanelContent
        openToThe="right"
        panel={{ title: '', content: <div />, isBeta: true }}
        dispatch={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Close Panel with Chevron
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Panel' })
        .props.onClick()
    })

    expect(mockFn).toHaveBeenCalledWith({
      type: 'CLOSE',
      payload: { openPanel: '' },
    })
  })

  it('should render properly to the left', () => {
    const component = TestRenderer.create(
      <PanelContent
        openToThe="left"
        panel={{ title: '', content: <div /> }}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
