import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher, revalidate } from '../Fetch/helpers'

import Modal from '../Modal'

const ModelDeleteModal = ({
  projectId,
  modelId,
  isDeleteModalOpen,
  setDeleteModalOpen,
}) => {
  const [isDeleting, setIsDeleting] = useState(false)

  if (!isDeleteModalOpen) return null

  return (
    <Modal
      title="Delete Model"
      message="Deleting this model cannot be undone."
      action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
      onCancel={() => {
        setDeleteModalOpen(false)
      }}
      onConfirm={async () => {
        setIsDeleting(true)

        await fetcher(`/api/v1/projects/${projectId}/models/${modelId}/`, {
          method: 'DELETE',
        })

        revalidate({
          key: `/api/v1/projects/${projectId}/models/all/`,
        })

        await revalidate({
          key: `/api/v1/projects/${projectId}/models/`,
        })

        Router.push(
          '/[projectId]/models?action=delete-model-success',
          `/${projectId}/models`,
        )
      }}
    />
  )
}

ModelDeleteModal.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  isDeleteModalOpen: PropTypes.bool.isRequired,
  setDeleteModalOpen: PropTypes.func.isRequired,
}

export default ModelDeleteModal
