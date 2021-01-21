import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import ModelMatrixShortcut, { noop } from '../Shortcut'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

jest.mock('next/link', () => 'Link')

describe('<ModelMatrixShortcut />', () => {
  it('should render properly when a matrix exists', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <ModelMatrixShortcut
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        matrix={matrix}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ children: 'View Matrix' }).props.onClick()
    })
  })

  it('should render properly when a matrix does not yet exist', () => {
    const component = TestRenderer.create(
      <ModelMatrixShortcut
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        matrix={{ ...matrix, matrix: [] }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a matrix will never exist', () => {
    const component = TestRenderer.create(
      <ModelMatrixShortcut
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        matrix={{ ...matrix, isMatrixApplicable: false }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
