import TestRenderer, { act } from 'react-test-renderer'

import FaceLabelingTrainApply from '../TrainApply'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '1a1d45af-7477-1396-ae57-a618e8efb91f'

jest.mock('../Message', () => 'FaceLebalingMessage')

const noop = () => () => {}

describe('<FaceLabelingTrainApply />', () => {
  it('should render properly when unapplied changes is true and there is no training job', () => {
    require('swr').__setMockUseSWRResponse({
      data: { unappliedChanges: false, jobId: '' },
    })

    const component = TestRenderer.create(
      <FaceLabelingTrainApply projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Train & Apply' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when unapplied changes is false and there is a training job', () => {
    require('swr').__setMockUseSWRResponse({
      data: { unappliedChanges: true, jobId: JOB_ID },
    })

    const component = TestRenderer.create(
      <FaceLabelingTrainApply projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onFocus()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onBlur()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onMouseEnter()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Training Help' })
        .props.onMouseLeave()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
