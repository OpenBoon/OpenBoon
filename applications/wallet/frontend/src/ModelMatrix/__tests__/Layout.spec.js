import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixLayout from '../Layout'

jest.mock('../Controls', () => 'ModelMatrixControls')
jest.mock('../Matrix', () => 'ModelMatrixMatrix')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelMatrixLayout />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <ModelMatrixLayout projectId={PROJECT_ID} modelId={MODEL_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Preview' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
