import TestRenderer from 'react-test-renderer'

import MetadataCuesContent from '../Content'

describe('<MetadataCuesContent />', () => {
  it('should render properly with no metadata', () => {
    const component = TestRenderer.create(
      <MetadataCuesContent metadata={{}} height={300} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with metadata', () => {
    const component = TestRenderer.create(
      <MetadataCuesContent
        metadata={{
          'gcp-label-detection': [
            { label: 'cheese', score: 0.91 },
            { label: 'pepperoni', score: 0.91 },
          ],
          'gcp-object-detection': [{ label: 'food', score: 0.91 }],
        }}
        height={300}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})