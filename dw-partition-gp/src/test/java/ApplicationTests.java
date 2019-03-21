
import me.w1992wishes.dwpartition.PartitionGpApp;
import me.w1992wishes.dwpartition.service.BaseService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PartitionGpApp.class)
public class ApplicationTests {

    @Resource(name = "odlService")
    private BaseService odlService;

    @Resource(name = "dwdService")
    private BaseService dwdService;

	@Test
	@Rollback
	public void createOdlEventFaceTable() {
        odlService.createEventFaceTables();
        Assert.assertEquals(1, 1);
	}

    @Test
    @Rollback
    public void addOdlOneDayPartitions() {
        odlService.addDayPartitions(LocalDateTime.now().plusDays(1), 3);
        Assert.assertEquals(1, 1);
    }

    @Test
    @Rollback
    public void deleteOdlEventFaceTable() {
        odlService.deleteTables();
        Assert.assertEquals(1, 1);
    }

    //---------------------------------------------------------- dwd ---------------------------------------------------

    @Test
    @Rollback
    public void createDwdEventFaceTable() {
        dwdService.createEventFaceTables();
        Assert.assertEquals(1, 1);
    }

    @Test
    @Rollback
    public void addDwdOneDayPartitions() {
        dwdService.addDayPartitions(LocalDateTime.now().plusDays(1), 3);
        Assert.assertEquals(1, 1);
    }

    @Test
    @Rollback
    public void deleteDwdEventFaceTable() {
        dwdService.deleteTables();
        Assert.assertEquals(1, 1);
    }

}

