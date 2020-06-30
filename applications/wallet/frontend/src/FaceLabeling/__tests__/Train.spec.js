import TestRenderer, { act } from 'react-test-renderer'

import FaceLabelingTrain from '../Train'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<FaceLabelingTrain />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <FaceLabelingTrain
        projectId={PROJECT_ID}
        isTraining
        showHelpInfo={false}
        isDisabled={false}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onKeyPress()
    })

    expect(mockDispatch.mock.calls[0][0]).toEqual({ showHelpInfo: true })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onMouseEnter()
    })

    expect(mockDispatch.mock.calls[1][0]).toEqual({ showHelpInfo: true })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onMouseLeave()
    })

    expect(mockDispatch.mock.calls[2][0]).toEqual({ showHelpInfo: false })
  })

  it('should properly render help information', () => {
    const component = TestRenderer.create(
      <FaceLabelingTrain
        projectId={PROJECT_ID}
        isTraining
        showHelpInfo
        isDisabled={false}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
