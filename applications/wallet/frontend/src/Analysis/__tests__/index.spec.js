import TestRenderer from 'react-test-renderer'

import bboxAsset from '../../Asset/__mocks__/bboxAsset'

import Analysis from '..'

jest.mock('../Classification', () => 'AnalysisClassification')

describe('<Analysis />', () => {
  it('should render properly', () => {
    const { analysis } = bboxAsset.metadata

    const component = TestRenderer.create(<Analysis analysis={analysis} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
