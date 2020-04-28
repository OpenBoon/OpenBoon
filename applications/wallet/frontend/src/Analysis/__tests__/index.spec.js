import TestRenderer from 'react-test-renderer'

import bboxAsset from '../../Asset/__mocks__/bboxAsset'

import MetadataAnalysis from '..'

jest.mock('../Classification', () => 'MetadataAnalysisClassification')

describe('<MetadataAnalysis />', () => {
  it('should render properly', () => {
    const { analysis } = bboxAsset.metadata

    const component = TestRenderer.create(
      <MetadataAnalysis analysis={analysis} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
