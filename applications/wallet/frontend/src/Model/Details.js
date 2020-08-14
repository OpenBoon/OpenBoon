import { useState } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { typography, spacing } from '../Styles'

import { encode } from '../Filters/helpers'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Modal from '../Modal'
import Tabs from '../Tabs'

import { onTrain, onDelete } from './helpers'

const LINE_HEIGHT = '23px'

const ModelDetails = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const [error, setError] = useState('')

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const { name, type, moduleName, ready, runningJobId } = model

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {},
      },
    ],
  })

  return (
    <div>
      {runningJobId && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            &quot;{name}&quot; training in progress.{' '}
            <Link
              href="/[projectId]/jobs/[jobId]"
              as={`/${projectId}/jobs/${runningJobId}`}
              passHref
            >
              <a>Check Status</a>
            </Link>
          </FlashMessage>
        </div>
      )}

      {error && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
        </div>
      )}

      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: typography.size.medium,
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <strong>Model Name:</strong> {name}
        </li>
        <li>
          <strong>Model Type:</strong> {type}
        </li>
        <li>
          <strong>Module Name:</strong> {moduleName}
        </li>
      </ul>

      <ButtonGroup>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() =>
            onTrain({ model, apply: false, projectId, modelId, setError })
          }
          isDisabled={ready}
        >
          Train
        </Button>

        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() =>
            onTrain({ model, apply: true, projectId, modelId, setError })
          }
          isDisabled={ready}
        >
          Train &amp; Apply
        </Button>

        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => {
            setDeleteModalOpen(true)
          }}
          isDisabled={false}
        >
          Delete
        </Button>

        {isDeleteModalOpen && (
          <Modal
            title="Delete Model"
            message="Deleting this model cannot be undone."
            action="Delete Permanently"
            onCancel={() => {
              setDeleteModalOpen(false)
            }}
            onConfirm={onDelete({ setDeleteModalOpen, projectId, modelId })}
          />
        )}
      </ButtonGroup>

      <Tabs
        tabs={[{ title: 'View Labels', href: '/[projectId]/models/[modelId]' }]}
      />

      <div
        css={{
          display: 'flex',
          justifyContent: 'flex-end',
          marginBottom: -spacing.normal,
        }}
      >
        <Link
          href={`/[projectId]/visualizer?query=${encodedFilter}`}
          as={`/${projectId}/visualizer?query=${encodedFilter}`}
          passHref
        >
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              localStorage.setItem('rightOpeningPanel', '"filters"')
            }}
          >
            Add Label Filter &amp; View in Visualizer
          </Button>
        </Link>
      </div>
    </div>
  )
}

export default ModelDetails
