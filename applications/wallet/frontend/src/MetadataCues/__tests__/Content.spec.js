import TestRenderer from 'react-test-renderer'

import MetadataCuesContent from '../Content'

describe('<MetadataCuesContent />', () => {
  it('should render properly with no metadata', () => {
    const component = TestRenderer.create(
      <MetadataCuesContent metadata={{}} width={300} height={500} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with metadata', () => {
    const component = TestRenderer.create(
      <MetadataCuesContent
        metadata={{
          'gcp-label-detection': [
            { content: 'cheese', score: 0.91 },
            { content: 'pepperoni', score: 0.91 },
          ],
          'gcp-object-detection': [{ content: 'food', score: 0.91 }],
          'gcp-logo-detection': [{ content: 'boon', score: 0.91 }],
        }}
        width={300}
        height={500}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
