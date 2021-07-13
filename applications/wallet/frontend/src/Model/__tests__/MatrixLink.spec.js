import TestRenderer from 'react-test-renderer'

import model from '../__mocks__/model'
import matrix from '../../ModelMatrix/__mocks__/matrix'

import ModelMatrixLink from '../MatrixLink'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<ModelMatrixLink />', () => {
  it('should render properly when a matrix is out of date', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: true,
          datasetId: DATASET_ID,
          timeLastTrained: 1625774562852,
          timeLastApplied: 1625774664673,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a matrix exists', () => {
    require('swr').__setMockUseSWRResponse({ data: matrix })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: DATASET_ID,
          timeLastTrained: 1625774562852,
          timeLastApplied: 1625774664673,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it("should render properly when the model has been applied but the matrix isn't ready", () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, matrix: [] },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: DATASET_ID,
          timeLastTrained: 1625774562852,
          timeLastApplied: 1625774664673,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when the model has not yet been applied', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, matrix: [] },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: DATASET_ID,
          timeLastTrained: 1625774562852,
          timeLastApplied: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when the model has not yet been trained', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, matrix: [] },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: DATASET_ID,
          timeLastTrained: 0,
          timeLastApplied: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a dataset has not yet been linked', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, matrix: [] },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: '',
          timeLastTrained: 0,
          timeLastApplied: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a matrix will never exist', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, isMatrixApplicable: false },
    })

    const component = TestRenderer.create(
      <ModelMatrixLink
        projectId={PROJECT_ID}
        model={{
          ...model,
          unappliedChanges: false,
          datasetId: '',
          timeLastTrained: 0,
          timeLastApplied: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
