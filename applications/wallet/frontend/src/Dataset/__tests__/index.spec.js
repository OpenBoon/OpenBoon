import TestRenderer, { act } from 'react-test-renderer'

import dataset from '../__mocks__/dataset'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Dataset from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../../DatasetLabels', () => 'DatasetLabels')
jest.mock('../../DatasetModels', () => 'DatasetModels')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<Dataset />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'edit-concept-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select Delete
    act(() => {
      component.root.findByProps({ children: 'Delete Dataset' }).props.onClick()
    })

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Delete
    act(() => {
      component.root.findByProps({ children: 'Delete Dataset' }).props.onClick()
    })

    // Confirm
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'DELETE',
    })
  })

  it('should render properly in edit concept mode', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'delete-concept-success',
        edit: 'cat',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should route properly to labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/labels',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        action: 'remove-asset-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should route properly to models', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/models',
      query: {
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Dataset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
