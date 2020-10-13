import TestRenderer from 'react-test-renderer'

import ResizeableVerticalDropMessage from '../DropMessage'

describe('<ResizeableVerticalDropMessage />', () => {
  it('should render properly when dragging down', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDropMessage size={200} originSize={300} />,
    )

    expect(component.root.findByType('div').props.children).toEqual([
      'Release to ',
      'collapse',
      '.',
    ])
  })

  it('should render properly when dragging up', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDropMessage size={200} originSize={0} />,
    )

    expect(component.root.findByType('div').props.children).toEqual([
      'Release to ',
      'expand',
      '.',
    ])
  })
})
