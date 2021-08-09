import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import Modal from '../Modal'

import { fetcher, revalidate } from '../Fetch/helpers'

const DatasetDeleteModal = ({
  projectId,
  datasetId,
  name,
  isDeleteModalOpen,
  setDeleteModalOpen,
  setDatasetFields,
}) => {
  const [isDeleting, setIsDeleting] = useState(false)

  if (!isDeleteModalOpen) return null

  return (
    <Modal
      title="Delete Dataset"
      message={`Are you sure you want to delete the "${name}" dataset? This will remove it from all linked models and cannot be undone.`}
      action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
      onCancel={() => {
        setDeleteModalOpen(false)
      }}
      onConfirm={async () => {
        setIsDeleting(true)

        setDatasetFields({ datasetId: '', labels: {} })

        await fetcher(`/api/v1/projects/${projectId}/datasets/${datasetId}/`, {
          method: 'DELETE',
        })

        revalidate({ key: `/api/v1/projects/${projectId}/datasets/all/` })

        await revalidate({ key: `/api/v1/projects/${projectId}/datasets/` })

        Router.push(
          '/[projectId]/datasets?action=delete-dataset-success',
          `/${projectId}/datasets`,
        )
      }}
    />
  )
}

DatasetDeleteModal.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  isDeleteModalOpen: PropTypes.bool.isRequired,
  setDeleteModalOpen: PropTypes.func.isRequired,
  setDatasetFields: PropTypes.func.isRequired,
}

export default DatasetDeleteModal
