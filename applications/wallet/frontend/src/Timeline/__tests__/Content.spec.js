import TestRenderer from 'react-test-renderer'

import TimelineContent from '../Content'

describe('<TimelineContent />', () => {
  it('should render properly when dragging down', () => {
    const component = TestRenderer.create(
      <TimelineContent
        size={200}
        originSize={300}
        isOpen={false}
        videoRef={{}}
      />,
    )

    expect(component.root.findAllByType('div')[1].props.children).toEqual([
      'Release to ',
      'collapse',
      '.',
    ])
  })

  it('should render properly when dragging up', () => {
    const component = TestRenderer.create(
      <TimelineContent
        size={200}
        originSize={0}
        isOpen={false}
        videoRef={{}}
      />,
    )

    expect(component.root.findAllByType('div')[1].props.children).toEqual([
      'Release to ',
      'expand',
      '.',
    ])
  })
})
