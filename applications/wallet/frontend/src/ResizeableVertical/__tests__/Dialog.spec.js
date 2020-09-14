import TestRenderer from 'react-test-renderer'

import ResizeableVerticalDialog from '../Dialog'

describe('<ResizeableVertical />', () => {
  it('should not render when larger than minExpandedSize', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDialog
        size={400}
        startingSize={0}
        minExpandedSize={300}
      />,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should display collapse when dragging down', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDialog
        size={200}
        startingSize={300}
        minExpandedSize={300}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should display expand when dragging up', () => {
    const component = TestRenderer.create(
      <ResizeableVerticalDialog
        size={200}
        startingSize={0}
        minExpandedSize={300}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
