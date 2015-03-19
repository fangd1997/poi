/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hslf.model;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherSpgrRecord;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;

/**
 *  Represents a group of shapes.
 *
 * @author Yegor Kozlov
 */
public class HSLFGroupShape extends HSLFShape implements ShapeContainer<HSLFShape> {

    /**
      * Create a new ShapeGroup. This constructor is used when a new shape is created.
      *
      */
    public HSLFGroupShape(){
        this(null, null);
        _escherContainer = createSpContainer(false);
    }

    /**
      * Create a ShapeGroup object and initilize it from the supplied Record container.
      *
      * @param escherRecord       <code>EscherSpContainer</code> container which holds information about this shape
      * @param parent    the parent of the shape
      */
    protected HSLFGroupShape(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape> parent){
        super(escherRecord, parent);
    }

    /**
     * @return the shapes contained in this group container
     */
    public HSLFShape[] getShapes() {
        List<HSLFShape> shapeList = getShapeList();
        HSLFShape[] shapes = shapeList.toArray(new HSLFShape[shapeList.size()]);
        return shapes;
    }

    /**
     * Sets the anchor (the bounding box rectangle) of this shape.
     * All coordinates should be expressed in Master units (576 dpi).
     *
     * @param anchor new anchor
     */
    public void setAnchor(java.awt.Rectangle anchor){

        EscherClientAnchorRecord clientAnchor = getEscherChild(EscherClientAnchorRecord.RECORD_ID);
        //hack. internal variable EscherClientAnchorRecord.shortRecord can be
        //initialized only in fillFields(). We need to set shortRecord=false;
        byte[] header = new byte[16];
        LittleEndian.putUShort(header, 0, 0);
        LittleEndian.putUShort(header, 2, 0);
        LittleEndian.putInt(header, 4, 8);
        clientAnchor.fillFields(header, 0, null);

        clientAnchor.setFlag((short)(anchor.y*MASTER_DPI/POINT_DPI));
        clientAnchor.setCol1((short)(anchor.x*MASTER_DPI/POINT_DPI));
        clientAnchor.setDx1((short)((anchor.width + anchor.x)*MASTER_DPI/POINT_DPI));
        clientAnchor.setRow1((short)((anchor.height + anchor.y)*MASTER_DPI/POINT_DPI));

        EscherSpgrRecord spgr = getEscherChild(EscherSpgrRecord.RECORD_ID);

        spgr.setRectX1(anchor.x*MASTER_DPI/POINT_DPI);
        spgr.setRectY1(anchor.y*MASTER_DPI/POINT_DPI);
        spgr.setRectX2((anchor.x + anchor.width)*MASTER_DPI/POINT_DPI);
        spgr.setRectY2((anchor.y + anchor.height)*MASTER_DPI/POINT_DPI);
    }

    /**
     * Sets the coordinate space of this group.  All children are constrained
     * to these coordinates.
     *
     * @param anchor the coordinate space of this group
     */
    public void setCoordinates(Rectangle2D anchor){
        EscherSpgrRecord spgr = getEscherChild(EscherSpgrRecord.RECORD_ID);

        int x1 = (int)Math.round(anchor.getX()*MASTER_DPI/POINT_DPI);
        int y1 = (int)Math.round(anchor.getY()*MASTER_DPI/POINT_DPI);
        int x2 = (int)Math.round((anchor.getX() + anchor.getWidth())*MASTER_DPI/POINT_DPI);
        int y2 = (int)Math.round((anchor.getY() + anchor.getHeight())*MASTER_DPI/POINT_DPI);

        spgr.setRectX1(x1);
        spgr.setRectY1(y1);
        spgr.setRectX2(x2);
        spgr.setRectY2(y2);

    }

    /**
     * Gets the coordinate space of this group.  All children are constrained
     * to these coordinates.
     *
     * @return the coordinate space of this group
     */
    public Rectangle2D getCoordinates(){
        EscherSpgrRecord spgr = getEscherChild(EscherSpgrRecord.RECORD_ID);

        Rectangle2D.Float anchor = new Rectangle2D.Float();
        anchor.x = (float)spgr.getRectX1()*POINT_DPI/MASTER_DPI;
        anchor.y = (float)spgr.getRectY1()*POINT_DPI/MASTER_DPI;
        anchor.width = (float)(spgr.getRectX2() - spgr.getRectX1())*POINT_DPI/MASTER_DPI;
        anchor.height = (float)(spgr.getRectY2() - spgr.getRectY1())*POINT_DPI/MASTER_DPI;

        return anchor;
    }

    /**
     * Create a new ShapeGroup and create an instance of <code>EscherSpgrContainer</code> which represents a group of shapes
     */
    protected EscherContainerRecord createSpContainer(boolean isChild) {
        EscherContainerRecord spgr = new EscherContainerRecord();
        spgr.setRecordId(EscherContainerRecord.SPGR_CONTAINER);
        spgr.setOptions((short)15);

        //The group itself is a shape, and always appears as the first EscherSpContainer in the group container.
        EscherContainerRecord spcont = new EscherContainerRecord();
        spcont.setRecordId(EscherContainerRecord.SP_CONTAINER);
        spcont.setOptions((short)15);

        EscherSpgrRecord spg = new EscherSpgrRecord();
        spg.setOptions((short)1);
        spcont.addChildRecord(spg);

        EscherSpRecord sp = new EscherSpRecord();
        short type = (short)((ShapeType.NOT_PRIMITIVE.nativeId << 4) + 2);
        sp.setOptions(type);
        sp.setFlags(EscherSpRecord.FLAG_HAVEANCHOR | EscherSpRecord.FLAG_GROUP);
        spcont.addChildRecord(sp);

        EscherClientAnchorRecord anchor = new EscherClientAnchorRecord();
        spcont.addChildRecord(anchor);

        spgr.addChildRecord(spcont);
        return spgr;
    }

    /**
     * Add a shape to this group.
     *
     * @param shape - the Shape to add
     */
    public void addShape(HSLFShape shape){
        _escherContainer.addChildRecord(shape.getSpContainer());

        HSLFSheet sheet = getSheet();
        shape.setSheet(sheet);
        shape.setShapeId(sheet.allocateShapeId());
        shape.afterInsert(sheet);
    }

    /**
     * Moves this <code>ShapeGroup</code> to the specified location.
     * <p>
     * @param x the x coordinate of the top left corner of the shape in new location
     * @param y the y coordinate of the top left corner of the shape in new location
     */
    public void moveTo(int x, int y){
        java.awt.Rectangle anchor = getAnchor();
        int dx = x - anchor.x;
        int dy = y - anchor.y;
        anchor.translate(dx, dy);
        setAnchor(anchor);

        HSLFShape[] shape = getShapes();
        for (int i = 0; i < shape.length; i++) {
            java.awt.Rectangle chanchor = shape[i].getAnchor();
            chanchor.translate(dx, dy);
            shape[i].setAnchor(chanchor);
        }
    }

    /**
     * Returns the anchor (the bounding box rectangle) of this shape group.
     * All coordinates are expressed in points (72 dpi).
     *
     * @return the anchor of this shape group
     */
    public Rectangle2D getAnchor2D(){
        EscherClientAnchorRecord clientAnchor = getEscherChild(EscherClientAnchorRecord.RECORD_ID);
        Rectangle2D.Float anchor = new Rectangle2D.Float();
        if(clientAnchor == null){
            logger.log(POILogger.INFO, "EscherClientAnchorRecord was not found for shape group. Searching for EscherChildAnchorRecord.");
            EscherChildAnchorRecord rec = getEscherChild(EscherChildAnchorRecord.RECORD_ID);
            anchor = new Rectangle2D.Float(
                (float)rec.getDx1()*POINT_DPI/MASTER_DPI,
                (float)rec.getDy1()*POINT_DPI/MASTER_DPI,
                (float)(rec.getDx2()-rec.getDx1())*POINT_DPI/MASTER_DPI,
                (float)(rec.getDy2()-rec.getDy1())*POINT_DPI/MASTER_DPI
            );
        } else {
            anchor.x = (float)clientAnchor.getCol1()*POINT_DPI/MASTER_DPI;
            anchor.y = (float)clientAnchor.getFlag()*POINT_DPI/MASTER_DPI;
            anchor.width = (float)(clientAnchor.getDx1() - clientAnchor.getCol1())*POINT_DPI/MASTER_DPI ;
            anchor.height = (float)(clientAnchor.getRow1() - clientAnchor.getFlag())*POINT_DPI/MASTER_DPI;
        }

        return anchor;
    }

    /**
     * Return type of the shape.
     * In most cases shape group type is {@link org.apache.poi.hslf.model.ShapeTypes#NotPrimitive}
     *
     * @return type of the shape.
     */
    public ShapeType getShapeType(){
        EscherSpRecord spRecord = getEscherChild(EscherSpRecord.RECORD_ID);
        int nativeId = spRecord.getOptions() >> 4;
        return ShapeType.forId(nativeId, false);
    }

    /**
     * Returns <code>null</code> - shape groups can't have hyperlinks
     *
     * @return <code>null</code>.
     */
     public Hyperlink getHyperlink(){
        return null;
    }

    public void draw(Graphics2D graphics){

        AffineTransform at = graphics.getTransform();

        HSLFShape[] sh = getShapes();
        for (int i = 0; i < sh.length; i++) {
            sh[i].draw(graphics);
        }

        graphics.setTransform(at);
    }

    @Override
    public <T extends EscherRecord> T getEscherChild(int recordId){
        EscherContainerRecord groupInfoContainer = (EscherContainerRecord)_escherContainer.getChild(0);
        return groupInfoContainer.getChildById((short)recordId);
    }

    public Iterator<HSLFShape> iterator() {
        return getShapeList().iterator();
    }

    public boolean removeShape(HSLFShape shape) {
        // TODO: implement!
        throw new UnsupportedOperationException();
    }

    /**
     * @return the shapes contained in this group container
     */
    protected List<HSLFShape> getShapeList() {
        // Out escher container record should contain several
        //  SpContainers, the first of which is the group shape itself
        Iterator<EscherRecord> iter = _escherContainer.getChildIterator();

        // Don't include the first SpContainer, it is always NotPrimitive
        if (iter.hasNext()) {
            iter.next();
        }
        List<HSLFShape> shapeList = new ArrayList<HSLFShape>();
        while (iter.hasNext()) {
            EscherRecord r = iter.next();
            if(r instanceof EscherContainerRecord) {
                // Create the Shape for it
                EscherContainerRecord container = (EscherContainerRecord)r;
                HSLFShape shape = ShapeFactory.createShape(container, this);
                shape.setSheet(getSheet());
                shapeList.add( shape );
            } else {
                // Should we do anything special with these non
                //  Container records?
                logger.log(POILogger.ERROR, "Shape contained non container escher record, was " + r.getClass().getName());
            }
        }

        return shapeList;
    }

}
