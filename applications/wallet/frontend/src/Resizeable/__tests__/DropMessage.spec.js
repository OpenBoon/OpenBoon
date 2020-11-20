import TestRenderer from 'react-test-renderer'

import ResizeableVerticalDropMessage from '../DropMessage'

describe('<ResizeableVerticalDropMessage />', () => {
  it('should render properly when dragging close', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDropMessage
        size={200}
        originSize={300}
        isHorizontal={false}
      />,
    )

    expect(component.root.findByType('div').props.children).toEqual([
      'Release to ',
      'collapse',
      '.',
    ])
  })

  it('should render properly when dragging open', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDropMessage size={200} originSize={0} isHorizontal />,
    )

    expect(component.root.findByType('div').props.children).toEqual([
      'Release to ',
      'expand',
      '.',
    ])
  })
})
