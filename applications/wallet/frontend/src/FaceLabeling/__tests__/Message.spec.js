import TestRenderer, { act } from 'react-test-renderer'

import FaceLabelingMessage from '../Message'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '1a1d45af-7477-1396-ae57-a618e8efb91f'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

describe('<FaceLabelingMessage />', () => {
  it('should render properly when a training job just started', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        jobId={JOB_ID}
        error=""
        setPreviousJobId={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith(JOB_ID)
  })

  it('should render properly when a training job just finished', () => {
    const mockCacheDeleteFn = jest.fn()

    require('swr').__setMockCacheKeys([`/faces/${ASSET_ID}/`, '/faces/status/'])

    require('swr').__setMockCacheDeleteFn(mockCacheDeleteFn)

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId={JOB_ID}
        jobId=""
        error=""
        setPreviousJobId={mockFn}
      />,
    )

    // useEffect
    act(() => {})

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).not.toHaveBeenCalled()

    expect(mockCacheDeleteFn).toHaveBeenCalledTimes(1)

    expect(mockCacheDeleteFn).toHaveBeenCalledWith(`/faces/${ASSET_ID}/`)
  })

  it('should render properly when there is no previous or current job', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        jobId=""
        error=""
        setPreviousJobId={mockFn}
      />,
    )

    expect(component.toJSON()).toEqual(null)

    expect(mockFn).not.toHaveBeenCalled()
  })

  it('should render properly when there is an error', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        jobId=""
        error="Error"
        setPreviousJobId={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).not.toHaveBeenCalled()
  })
})
