import TestRenderer from 'react-test-renderer'

import matrix from '../../ModelMatrix/__mocks__/matrix'

import ModelMatrixLink from '../MatrixLink'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelMatrixLink />', () => {
  it('should render properly when a matrix exists', () => {
    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <ModelMatrixLink projectId={PROJECT_ID} modelId={MODEL_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a matrix does not yet exist', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, matrix: [] },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink projectId={PROJECT_ID} modelId={MODEL_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a matrix will never exist', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, isMatrixApplicable: false },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink projectId={PROJECT_ID} modelId={MODEL_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
