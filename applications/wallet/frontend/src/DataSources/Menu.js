import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const DataSourcesMenu = ({ projectId, dataSourceId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <Menu open="left" button={ButtonActions}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <Link
                href="/[projectId]/data-sources/[dataSourceId]/edit"
                as={`/${projectId}/data-sources/${dataSourceId}/edit`}
                passHref
              >
                <Button variant={VARIANTS.MENU_ITEM}>Edit</Button>
              </Link>
            </li>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}
                >
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    title="Delete Data Source"
                    message="Deleting this data source cannot be undone."
                    action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
                    onCancel={() => {
                      setDeleteModalOpen(false)
                      onClick()
                    }}
                    onConfirm={async () => {
                      setIsDeleting(true)

                      await fetcher(
                        `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`,
                        { method: 'DELETE' },
                      )

                      await revalidate()

                      Router.push(
                        '/[projectId]/data-sources?action=delete-datasource-success',
                        `/${projectId}/data-sources`,
                      )
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

DataSourcesMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSourceId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DataSourcesMenu
